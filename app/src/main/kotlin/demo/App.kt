package demo

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import kotlinx.ast.common.AstSource
import kotlinx.ast.common.ast.Ast
import kotlinx.ast.common.ast.DefaultAstNode
import kotlinx.ast.common.klass.KlassDeclaration
import kotlinx.ast.common.print
import kotlinx.ast.grammar.kotlin.common.summary
import kotlinx.ast.grammar.kotlin.target.antlr.kotlin.KotlinGrammarAntlrKotlinParser
import kotlin.reflect.KClass

fun main() {
    val source = AstSource.File(
        "./app/src/main/resources/Repository.kt"
    )
    val kotlinFile = KotlinGrammarAntlrKotlinParser.parseKotlinFile(source)
    kotlinFile.summary(attachRawAst = false)
        .onSuccess { astList ->
            astList.forEach(Ast::print)
            val someClass = astList.filterIsInstance<KlassDeclaration>().first()
            klassToInterface(someClass).writeTo(System.out)
        }.onFailure { errors ->
            errors.forEach(::println)
        }
//    print(kotlinFile.printString())
//    val ktInterface =
}

fun klassToInterface(klass: KlassDeclaration): FileSpec {
    val name = klass.identifier!!.identifier + "Interface"
    val interfaceBuilder = TypeSpec.interfaceBuilder(name)
    for (f in (klass.children[0] as DefaultAstNode).children.filter { (it as KlassDeclaration).keyword == "fun" }) {
        val funBuilder = FunSpec.builder((f as KlassDeclaration).identifier!!.identifier)
//        f.modifiers.forEach {
////            funBuilder.addModifiers(it.)
//        }
        f.parameter.forEach {
            funBuilder.addParameter(it.identifier!!.rawName, Class.forName((it.type[0].rawName)))
        }
        funBuilder.returns(Class.forName(f.type[0].identifier))
        interfaceBuilder.addFunction(funBuilder.build())
    }

    return FileSpec.builder("out", name).addType(interfaceBuilder.build()).build()
}
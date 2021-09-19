//package demo
//
//import com.squareup.kotlinpoet.JavaFile
//import com.squareup.kotlinpoet.MethodSpec
//import com.squareup.kotlinpoet.ParameterSpec
//import com.squareup.kotlinpoet.TypeSpec
//import java.io.IOException
//import java.lang.reflect.Method
//import java.lang.reflect.Parameter
//import java.net.MalformedURLException
//import java.net.URL
//import java.net.URLClassLoader
//import java.nio.file.Path
//import java.util.*
//import java.util.function.Function
//import java.util.stream.Collectors
//import javax.lang.model.element.Modifier
//
//
//class App2 {
//}
//
//
//
//class Interfacor(private val outputDirectory: String) {
//    fun implementFromDirectory(directoryPath: String?, className: String?): String? {
//        // load class
//        val url: URL = try {
//            Path.of(directoryPath).toUri().toURL()
//        } catch (e: MalformedURLException) {
//            assert(false)
//            return null
//        }
//        val loader = URLClassLoader(arrayOf(url))
//        val clazz: Class<*> = try {
//            loader.loadClass(className)
//        } catch (e: ClassNotFoundException) {
//            throw Exception("Входной класс не найден", e)
//        }
//
//        // make implementation
//        val classBuilder: TypeSpec.Builder = makeImpl(clazz)
//        val javaFile: JavaFile = JavaFile.builder(clazz.packageName, classBuilder.build()).build()
//
//        // write result to file
//        System.out.println(javaFile)
//        val dirPath = Path.of(outputDirectory)
//        dirPath.toFile().mkdir()
//        try {
//            javaFile.writeTo(dirPath)
//        } catch (e: IOException) {
//            throw ImplementorException("Невозможно записать сгенерированный класс", e)
//        }
//        return clazz.packageName + "." + getClassImplName(clazz)
//    }
//
//    private fun makeImpl(clazz: Class<*>): TypeSpec.Builder {
//        val methodsToInherit: Set<Method> = getMethodsToInherit(clazz)
//        val classBuilder: TypeSpec.Builder = TypeSpec.classBuilder(getClassImplName(clazz))
//        for (m in methodsToInherit) {
//            val mod = m.modifiers
//            val methodSpec: MethodSpec = MethodSpec.methodBuilder(m.name)
//                .addModifiers(toPoetModifiers(mod, true))
//                .returns(m.returnType)
//                .addParameters(
//                    Arrays.stream(m.parameters).map(
//                        Function<Parameter, ParameterSpec> { p: Parameter ->
//                            builder(
//                                p.type,
//                                p.name,
//                                toPoetModifiers(p.modifiers, true)
//                            ).build()
//                        }
//                    ).collect(Collectors.toSet())
//                )
//                .addStatement(methodBlockByType(m.returnType))
//                .build()
//            classBuilder.addMethod(methodSpec)
//        }
//        classBuilder.addModifiers(toPoetModifiers(clazz.modifiers, false))
//        if (clazz.isInterface) {
//            classBuilder.addSuperinterface(clazz)
//        } else {
//            classBuilder.superclass(clazz)
//        }
//        return classBuilder
//    }
//
//    // get all methods from clazz recursively
//    private fun getMethodsToInherit(clazz: Class<*>?): MutableSet<Method> {
//        if (clazz == null || clazz == Any::class.java) {
//            return HashSet()
//        }
//        val mod = clazz.modifiers
//        if (!(java.lang.reflect.Modifier.isInterface(mod) || java.lang.reflect.Modifier.isAbstract(mod))) {
//            throw Exception("Not an interface or an abstract class")
//        }
//        if (java.lang.reflect.Modifier.isFinal(mod)) {
//            throw Exception("Can't inherit final class")
//        }
//        if (clazz.isArray) {
//            throw Exception("Can't inherit array class")
//        }
//        val superclass = clazz.superclass
//        val methodsToInherit = getMethodsToInherit(superclass)
//        val interfaces = clazz.interfaces
//        for (i in interfaces) {
//            // there will be no diamond problem
//            val newInterfaceMethods: Set<Method> = getMethodsToInherit(i)
//            for (newMeth in newInterfaceMethods) {
//                removeCovariant(methodsToInherit, newMeth)
//                methodsToInherit.add(newMeth)
//            }
//        }
//        val newMethods = Arrays.stream(clazz.declaredMethods).filter { m: Method -> !m.isBridge && !m.isSynthetic }
//            .toArray<Method> { _Dummy_.__Array__() }
//        if (java.lang.reflect.Modifier.isInterface(mod)) {
//            for (newMeth in newMethods) {
//                removeCovariant(methodsToInherit, newMeth)
//                methodsToInherit.add(newMeth)
//            }
//        } else {
//            for (newMeth in newMethods) {
//                removeCovariant(methodsToInherit, newMeth)
//                val newMethMod = newMeth.modifiers
//                if (java.lang.reflect.Modifier.isAbstract(newMethMod)) {
//                    methodsToInherit.add(newMeth)
//                }
//            }
//        }
//        return methodsToInherit
//    }
//
//    private fun removeCovariant(methodSet: MutableSet<Method>, method: Method) {
//        for (currMeth in methodSet) {
//            if (areMethodsCovariant(currMeth, method)) {
//                methodSet.remove(currMeth)
//                break
//            }
//        }
//    }
//
//    private fun areMethodsCovariant(from: Method, to: Method): Boolean {
//        return if (from.name == to.name && Arrays.equals(from.parameterTypes, to.parameterTypes)) {
//            from.returnType.isAssignableFrom(to.returnType) && areAccessModifiersCovariant(from, to)
//        } else false
//    }
//
//    private fun areAccessModifiersCovariant(from: Method, to: Method): Boolean {
//        val fromMod = from.modifiers
//        val toMod = to.modifiers
//        return if (java.lang.reflect.Modifier.isProtected(fromMod)) {
//            java.lang.reflect.Modifier.isProtected(toMod) || java.lang.reflect.Modifier.isPublic(toMod)
//        } else java.lang.reflect.Modifier.isPublic(fromMod) && java.lang.reflect.Modifier.isPublic(toMod)
//    }
//
//    private fun toPoetModifiers(mod: Int, canBeStatic: Boolean): Array<Modifier> {
//        val modifiers: MutableList<Modifier> = ArrayList()
//        if (java.lang.reflect.Modifier.isPublic(mod)) modifiers.add(Modifier.PUBLIC)
//        if (java.lang.reflect.Modifier.isProtected(mod)) modifiers.add(Modifier.PROTECTED)
//        if (java.lang.reflect.Modifier.isPrivate(mod)) modifiers.add(Modifier.PRIVATE)
//        //        if (Modifier.isAbstract(mod)) modifiers.add(ABSTRACT);
//        if (canBeStatic && java.lang.reflect.Modifier.isStatic(mod)) modifiers.add(Modifier.STATIC)
//        return modifiers.toTypedArray()
//    }
//
//    private fun methodBlockByType(retClass: Class<*>): String {
//        if (retClass == Void.TYPE) {
//            return "return"
//        }
//        if (!retClass.isPrimitive) {
//            return "return null"
//        }
//        return if (retClass == Boolean::class.javaPrimitiveType) {
//            "return true"
//        } else "return 0"
//    }
//
//    private fun getClassImplName(clazz: Class<*>): String {
//        return clazz.simpleName + "Impl"
//    }
//}

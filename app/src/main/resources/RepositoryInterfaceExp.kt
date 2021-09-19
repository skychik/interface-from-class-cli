interface RepositoryInterfaceExp {
    fun get(id: Int): Loan
    fun getAll(): List<Loan>
    fun store(loan: Int): Int
    fun remove(loan: Int): Int
}
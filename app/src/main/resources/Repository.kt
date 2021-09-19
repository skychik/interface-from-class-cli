class Repository {
    fun get(id: Int): Loan {
        return Loan()
    }

    fun getAll(): List<Loan> {
        return emptyList()
    }

    fun store(loan: Int): Int {
        return 0;
    }

    fun remove(loan: Int): Int {
        return 0;
    }
}
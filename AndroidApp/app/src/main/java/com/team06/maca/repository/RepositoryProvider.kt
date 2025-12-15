package com.team06.maca.repository

object RepositoryProvider {
    val repository: GameRepository by lazy {
        RemoteRepository()
    }
}

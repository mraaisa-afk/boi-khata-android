package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.UserDao
import com.boikhata.core.database.entity.UserEntity
import com.boikhata.core.domain.enums.Role
import com.boikhata.core.domain.model.User
import com.boikhata.core.domain.repository.UserRepository
import com.boikhata.core.database.seed.DatabaseSeeder
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
) : UserRepository {

    override suspend fun getUsers(tenantId: String): List<User> {
        return userDao.getActiveByTenant(tenantId).map { it.toDomain() }
    }

    override suspend fun verifyPin(tenantId: String, pin: String): User? {
        val users = userDao.getActiveByTenant(tenantId)
        for (entity in users) {
            if (DatabaseSeeder.verifyPin(pin, entity.pinHash, entity.salt)) {
                return entity.toDomain()
            }
        }
        return null
    }

    private fun UserEntity.toDomain() = User(
        id = id,
        tenantId = tenantId,
        name = name,
        role = Role.valueOf(role),
        isActive = isActive,
    )
}

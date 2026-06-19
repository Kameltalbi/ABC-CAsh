package com.abccash.server.service

import com.abccash.server.db.Entreprises
import com.abccash.server.db.Users
import com.abccash.server.model.AuthResponse
import com.abccash.server.model.LoginRequest
import com.abccash.server.model.RegisterRequest
import com.abccash.server.model.UserDto
import com.abccash.server.security.PasswordHasher
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

class AuthService(private val config: ApplicationConfig) {

    private val jwtSecret: String
        get() = System.getenv("JWT_SECRET") ?: config.property("abc.jwtSecret").getString()
    private val issuer: String
        get() = config.property("abc.jwtIssuer").getString()
    private val audience: String
        get() = config.property("abc.jwtAudience").getString()

    fun register(request: RegisterRequest): Result<AuthResponse> {
        if (request.entrepriseNom.isBlank()) return Result.failure(IllegalArgumentException("Company name required"))
        if (request.nom.isBlank()) return Result.failure(IllegalArgumentException("Name required"))
        if (request.email.isBlank()) return Result.failure(IllegalArgumentException("Email required"))
        if (request.password.length < 6) return Result.failure(IllegalArgumentException("Password too short"))

        val email = request.email.trim().lowercase()
        val phone = request.telephone.replace("\\s".toRegex(), "")
        val now = Instant.now()

        val existing = transaction {
            Users.selectAll().where { Users.email eq email }.count()
        }
        if (existing > 0) return Result.failure(IllegalArgumentException("Email already used"))

        val entrepriseId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        val passwordHash = PasswordHasher.hash(request.password)
        val allPermissions = listOf(
            "VIEW_INVOICES", "ADD_PAYMENTS", "MANAGE_EXPENSES",
            "VIEW_TREASURY", "MANAGE_USERS"
        )

        transaction {
            Entreprises.insert {
                it[Entreprises.id] = entrepriseId
                it[Entreprises.nom] = request.entrepriseNom.trim()
                it[Entreprises.email] = email
                it[Entreprises.telephone] = phone
                it[Entreprises.adresse] = ""
                it[Entreprises.dateCreation] = now
                it[Entreprises.adminId] = userId
                it[Entreprises.updatedAt] = now
            }
            Users.insert {
                it[Users.id] = userId
                it[Users.entrepriseId] = entrepriseId
                it[Users.nom] = request.nom.trim()
                it[Users.email] = email
                it[Users.telephone] = phone
                it[Users.passwordHash] = passwordHash
                it[Users.role] = "ADMIN"
                it[Users.permissions] = allPermissions.joinToString(",")
                it[Users.dateInscription] = now
                it[Users.isActive] = true
                it[Users.updatedAt] = now
            }
        }

        val user = UserDto(
            id = userId,
            nom = request.nom.trim(),
            email = email,
            telephone = phone,
            role = "ADMIN",
            permissions = allPermissions,
            entrepriseId = entrepriseId
        )
        return Result.success(
            AuthResponse(
                token = createToken(userId, entrepriseId, "ADMIN"),
                user = user,
                entrepriseId = entrepriseId
            )
        )
    }

    fun login(request: LoginRequest): Result<AuthResponse> {
        val email = request.email.trim().lowercase()
        val row = transaction {
            Users.selectAll().where { Users.email eq email }.singleOrNull()
        } ?: return Result.failure(IllegalArgumentException("Invalid credentials"))

        if (!row[Users.isActive]) return Result.failure(IllegalArgumentException("Account disabled"))
        if (!PasswordHasher.verify(request.password, row[Users.passwordHash])) {
            return Result.failure(IllegalArgumentException("Invalid credentials"))
        }

        val user = row.toUserDto()
        return Result.success(
            AuthResponse(
                token = createToken(user.id, user.entrepriseId, user.role),
                user = user,
                entrepriseId = user.entrepriseId
            )
        )
    }

    fun createToken(userId: String, entrepriseId: String, role: String): String {
        val expiresAt = Instant.now().plusSeconds(60L * 60 * 24 * 30)
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            .withClaim("entrepriseId", entrepriseId)
            .withClaim("role", role)
            .withExpiresAt(expiresAt)
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toUserDto() = UserDto(
        id = this[Users.id],
        nom = this[Users.nom],
        email = this[Users.email],
        telephone = this[Users.telephone],
        role = this[Users.role],
        permissions = this[Users.permissions].split(',').filter { it.isNotBlank() },
        entrepriseId = this[Users.entrepriseId]
    )
}

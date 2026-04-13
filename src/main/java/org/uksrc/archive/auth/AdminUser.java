package org.uksrc.archive.auth;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;

/**
 * Represents an administrative user within the authentication system.
 * This entity is used to manage and authenticate admin-level users, supporting
 * login credentials, password hashes, and assigned roles.
 * <p>
 * The class is mapped to the "admin_users" table in the "auth" schema of the database,
 * and it uses annotations to define its behaviour within the authentication framework.
 * <p>
 * Key Functionalities:
 * - Supports adding a new administrative user through the {@code add} method.
 * - Automatically hashes plain-text passwords using the bcrypt hashing algorithm.
 * - Provides persistence support through the {@link PanacheEntity} base class.
 */
@Entity
@Table(name = "admin_users", schema = "auth")
@UserDefinition
public class AdminUser extends PanacheEntity {

    @Username
    @Column(name = "login")
    public String login;        // Username column

    @Password
    @Column(name = "pwd_hash")
    public String password;

    @Roles
    @Column(name = "roles_csv")
    public String roles;        // Comma-separated roles column

    /**
     * Adds a new administrative user to the authentication system by creating an
     * {@link AdminUser} instance, hashing the provided password, setting the specified
     * roles, and persisting the user to the database.
     *
     * @param login        the username for the new admin user
     * @param plainPassword the plaintext password that will be hashed and stored
     * @param rolesCsv     a comma-separated string of roles to assign to the user
     */
    @Transactional
    public static void add(String login, String plainPassword, String rolesCsv) {
        var user = new AdminUser();
        user.login = login;
        user.password = BcryptUtil.bcryptHash(plainPassword);
        user.roles = rolesCsv;
        user.persist();
    }
}

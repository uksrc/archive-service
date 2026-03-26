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

@Entity
@Table(name = "admin_users", schema = "auth")
@UserDefinition
public class AdminUser extends PanacheEntity {

    @Username
    @Column(name = "login")
    public String login;

    @Password
    @Column(name = "pwd_hash")
    public String password;

    @Roles
    @Column(name = "roles_csv")
    public String roles;

    @Transactional
    public static void add(String login, String plainPassword, String rolesCsv) {
        var user = new AdminUser();
        user.login = login;
        user.password = BcryptUtil.bcryptHash(plainPassword);
        user.roles = rolesCsv;
        user.persist();
    }
}

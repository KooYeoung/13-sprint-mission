package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserUpdateCommand;
import com.sprint.mission.discodeit.entity.base.UpdatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
@ToString(exclude = {"password", "userStatus", "profile"}, callSuper = true)
public class User extends UpdatableEntity {

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private BinaryContent profile;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "user")
    private UserStatus userStatus;

    public User(UserCreateCommand command, BinaryContent profile) {
        this.username = command.username();
        this.password = command.password();
        this.email = command.email();
        this.profile = profile;
    }

    public void updateInfo(
            UserUpdateCommand command, BinaryContent profile
    ) {
        this.username = keepIfBlank(command.username(), username);
        this.password = keepIfBlank(command.password(), password);
        this.email = keepIfBlank(command.email(), email);
        this.profile = profile;
    }

    public boolean isProfileImageExist() {
        return profile != null;
    }

    public boolean hasEmail(String email) {
        return this.email.equals(email);
    }

    public boolean hasUsername(String username) {
        return this.username.equals(username);
    }

    private String keepIfBlank(String newValue, String oldValue) {
        return StringUtils.defaultIfBlank(newValue, oldValue);
    }

    public UUID getProfileId() {
        if (profile == null) return null;

        return profile.getId();
    }

    public boolean isOnline() {
        if (userStatus == null) return false;
        return userStatus.isOnline();
    }

    public UUID getStatusId() {
        if (userStatus == null) return null;
        return userStatus.getId();
    }

    public void detachUserStatus() {
        this.userStatus = null;
    }
}

package me.confuser.banmanager.common.util;

import lombok.experimental.UtilityClass;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;

import java.util.UUID;

@UtilityClass
public class LuckPermsUtils {

    public static User getOrLoadUser(UUID uuid) {
        UserManager userManager = LuckPermsProvider.get().getUserManager();
        User user = userManager.getUser(uuid);
        if (user == null) {
            user = userManager.loadUser(uuid).join();
        }
        return user;
    }

}

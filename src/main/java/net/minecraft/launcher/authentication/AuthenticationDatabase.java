package net.minecraft.launcher.authentication;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraft.launcher.authentication.yggdrasil.YggdrasilAuthenticationService;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public class AuthenticationDatabase {
    public static class Serializer implements JsonDeserializer<AuthenticationDatabase>, JsonSerializer<AuthenticationDatabase> {
        public AuthenticationDatabase deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final TypeToken<Type> token = new TypeToken<Type>() {
            };
            final Map<String, AuthenticationService> services = new HashMap<String, AuthenticationService>();
            final Map<String, Map<String, String>> credentials = context.deserialize(json, token.getType());

            for(final Entry<String, Map<String, String>> entry : credentials.entrySet()) {
                final AuthenticationService service = new YggdrasilAuthenticationService();
                service.loadFromStorage(entry.getValue());
                services.put(entry.getKey(), service);
            }

            return new AuthenticationDatabase(services);
        }

        public JsonElement serialize(final AuthenticationDatabase src, final Type typeOfSrc, final JsonSerializationContext context) {
            final Map<String, AuthenticationService> services = src.authById;
            final Map<String, Map<String, String>> credentials = new HashMap<String, Map<String, String>>();

            for(final Entry<String, AuthenticationService> entry : services.entrySet())
                credentials.put(entry.getKey(), entry.getValue().saveForStorage());

            return context.serialize(credentials);
        }
    }

    public static final String DEMO_UUID_PREFIX = "demo-";

    public static String getUserFromDemoUUID(final String uuid) {
        if(uuid.startsWith("demo-") && uuid.length() > "demo-".length())
            return "Demo User " + uuid.substring("demo-".length());
        return "Demo User";
    }

    private final Map<String, AuthenticationService> authById;

    public AuthenticationDatabase() {
        this(new HashMap<String, AuthenticationService>());
    }

    public AuthenticationDatabase(final Map<String, AuthenticationService> authById) {
        this.authById = authById;
    }

    public AuthenticationService getByName(final String name) {
        if(name == null)
            return null;

        for(final Entry<String, AuthenticationService> entry : authById.entrySet()) {
            final GameProfile profile = entry.getValue().getSelectedProfile();

            if(profile != null && profile.getName().equals(name))
                return entry.getValue();
            if(profile == null && getUserFromDemoUUID(entry.getKey()).equals(name))
                return entry.getValue();
        }

        return null;
    }

    public AuthenticationService getByUUID(final String uuid) {
        return authById.get(uuid);
    }

    public Collection<String> getKnownNames() {
        final List<String> names = new ArrayList<String>();

        for(final Entry<String, AuthenticationService> entry : authById.entrySet()) {
            final GameProfile profile = entry.getValue().getSelectedProfile();

            if(profile != null)
                names.add(profile.getName());
            else
                names.add(getUserFromDemoUUID(entry.getKey()));
        }

        return names;
    }

    public Set<String> getknownUUIDs() {
        return authById.keySet();
    }

    public void register(final String uuid, final AuthenticationService authentication) {
        authById.put(uuid, authentication);
    }

    public void removeUUID(final String uuid) {
        authById.remove(uuid);
    }
}
package net.minecraft.launcher.authentication;

import java.lang.reflect.Type;
import java.util.Map;

import net.minecraft.launcher.authentication.yggdrasil.YggdrasilAuthenticationService;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class AuthenticationSerializer implements JsonDeserializer<AuthenticationService>, JsonSerializer<AuthenticationService> {
    public AuthenticationService deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        final AuthenticationService result = new YggdrasilAuthenticationService();
        if(json == null)
            return result;
        final Map<String, String> map = context.deserialize(json, Map.class);
        result.loadFromStorage(map);
        return result;
    }

    public JsonElement serialize(final AuthenticationService src, final Type typeOfSrc, final JsonSerializationContext context) {
        final Map<String, String> map = src.saveForStorage();
        if(map == null || map.isEmpty())
            return null;

        return context.serialize(map);
    }
}
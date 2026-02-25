package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class InternetUD extends BaseAcComponent {
    private final LuaVirtualMachine lvm;

    public InternetUD(LuaVirtualMachine lvm) {
        super("internet");
        this.lvm = lvm;
    }

    @LuaCallable
    public LuaObject get(String luaUrl) {
        URI url;
        try {
            url = URI.create(luaUrl);
        } catch (IllegalArgumentException ignored) {
            throw new LuaJavaError("Malformed request url");
        }
        String host = url.getHost().trim();
        if (host.contains(":")) { // must be ipv6
            throw new LuaJavaError("Requests to raw ipv6 addresses are not allowed for now");
        }

        var splittedV4 = host.split("\\.");
        if (host.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
            // this is an ipv4 address
            int[] ipArray = Arrays.stream(splittedV4).mapToInt(Integer::parseInt).toArray();

            // in part from https://en.wikipedia.org/wiki/List_of_reserved_IP_addresses
            int first = ipArray[0];
            int second = ipArray[1];
            int third = ipArray[2];
            boolean deny = (first == 0 || first == 10 || first == 127) // 0.*, 10.*, 127.*
                    || (first == 172 && (16 <= second && second <= 31)) // 172.[16,31].*
                    || (first == 192 && second == 168) // 192.168.*
                    || (first == 169 && second == 254) // 169.254.*
                    || (first == 224 && second == 239) // [224-239].*
                    || Arrays.stream(ipArray).allMatch(x -> x == 255) // 255.255.255.255
                    || (first == 192 && second == 0 && third == 0) // 192.0.0.*
                    || (first == 192 && second == 0 && third == 2) // 192.0.2.*
                    || (first == 198 && (second == 18 || second == 19)) // 198.[18-19].*
                    ;

            if (deny)
                throw new LuaJavaError("Request to local ip address %s is denied.".formatted(host));
        }

        HttpRequest req = HttpRequest.newBuilder(url).GET().build();
        var client = HttpClient.newHttpClient();

        try {
            var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            var rv = LuaObject.table();
            rv.set("status", LuaObject.of(resp.statusCode()));
            rv.set("body", LuaObject.of(resp.body()));
            return rv;
        } catch (IOException e) {
            throw new LuaJavaError("An error has occurred, check the log for more information");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LuaJavaError("An error has occurred, check the log for more information");
        }
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        // TODO actually provide serializaion
        return null;
    }

    @LuaDeserializer
    public static InternetUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        // TODO actually provide serializaion
        return null;
    }
}

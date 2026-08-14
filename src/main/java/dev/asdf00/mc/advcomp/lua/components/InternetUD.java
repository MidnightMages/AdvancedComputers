package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.*;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.Config;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.utils.RuntimeAssert;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class InternetUD extends BaseAcComponent {
    // TRACE is not allowed
    final static Set<String> allowedHttpMethods = Arrays.stream("GET;POST;HEAD;PUT;DELETE;OPTIONS;PATCH".split(";")).collect(Collectors.toSet());
    final static Set<String> httpMethodsAllowingReqBody = Arrays.stream("POST;PUT;DELETE;PATCH".split(";")).collect(Collectors.toSet());

    public InternetUD() {
        super("internet");
    }

    private InternetUD(LuaVirtualMachine acVm) {
        // an internet component, if present, is always available
        super("internet", acVm, true);
    }

    @LuaCallable
    public boolean isHttpEnabled() {
        return Config.componentInternetHttpEnabled;
    }

    @LuaCallable
    public HttpResponseUD sendHttpRequest(String url) {
        return sendHttpRequest(url, "GET", "", LuaObject.NIL);
    }

    @LuaCallable
    public HttpResponseUD sendHttpRequest(String url, String method) {
        return sendHttpRequest(url, method, "", LuaObject.NIL);
    }

    @LuaCallable
    public HttpResponseUD sendHttpRequest(String url, String method, String postData) {
        return sendHttpRequest(url, method, postData, LuaObject.NIL);
    }

    @LuaCallable
    public HttpResponseUD sendHttpRequest(String luaUrl, String method, String postData, LuaObject headers) {
        if (!isHttpEnabled())
            throw new LuaJavaError("HTTP is currently not enabled in the server's config!");

        // =============== URL ===============
        var urlMatcher = Pattern.compile("^(\\w+)://").matcher(luaUrl);
        if (urlMatcher.find()) {
            var protocol = urlMatcher.group(1).toLowerCase();
            if (!protocol.equals("https") && !protocol.equals("http")) {
                throw new LuaJavaError("Invalid http protocol '%s' in argument 1 (url)".formatted(protocol));
            }
            // else, url is prefixed with 'http(s)://' already
        } else {
            // if it doesnt have a prefix, add http; sites can redirect to https anyway
            luaUrl = "http://" + luaUrl;
        }

        // =============== METHOD ===============
        method = method.toUpperCase();
        if (!allowedHttpMethods.contains(method))
            throw new LuaJavaError("Expected argument 2 to be a valid http request method, such as 'GET', 'POST', or others.");

        // =============== POST DATA ===============
        if (!postData.isEmpty() && !httpMethodsAllowingReqBody.contains(method))
            throw new LuaJavaError(("Request method %s does not allow a request body, but argument 3 was a non-empty string. " +
                                    "Pick a different request method or supply an empty string.").formatted(method));

        // =============== HEADERS ===============
        var headersToSet = new HashMap<String, String>();
        if (headers.isTable()) {
            var table = headers.asMap();
            var headerKeys = table.keys();
            for (var headerKey : headerKeys) {
                if (!headerKey.isType(LuaObject.Types.ARITHMETIC)) {
                    throw new LuaJavaError("All header keys should be strings, encountered type %s".formatted(headerKey.getTypeAsString()));
                }

                var headerValue = table.getOrDefault(headerKey, LuaObject.NIL);
                RuntimeAssert.RuntimeAssert(!headerValue.isNil(), "somehow a value in the headertable value was nil?");
                if (!headerValue.isType(LuaObject.Types.ARITHMETIC)) {
                    throw new LuaJavaError("All header values should be strings, encountered type %s".formatted(headerValue.getTypeAsString()));
                }

                headersToSet.put(headerKey.asString().toLowerCase(), headerValue.asString());
            }
        } else if (!headers.isNil()) {
            throw new LuaJavaError("Expected argument 4 to be either table or nil");
        }
        headersToSet.put("User-Agent", "Minecraft/AdvancedComputers");


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

        if (Config.componentInternetBlockLocalIPs && host.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            // this is an ipv4 address
            int[] ipArray = Arrays.stream(host.split("\\.")).mapToInt(Integer::parseInt).toArray();

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

        var reqBuilder = HttpRequest
                .newBuilder(url)
                .method(method, HttpRequest.BodyPublishers.ofString(postData));
        for (var k : headersToSet.keySet()) {
            reqBuilder.header(k, headersToSet.get(k));
        }

        HttpRequest req = reqBuilder.build();
        var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

        return new HttpResponseUD(client.sendAsync(req, HttpResponse.BodyHandlers.ofString()));
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        // no internal state to serialize
        return null;
    }

    @SuppressWarnings("unused")
    @LuaDeserializer
    public static InternetUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return new InternetUD((LuaVirtualMachine) additionalData);
    }

    public static class HttpResponseUD implements LuaUserData {
        private HttpResponse<String> resp = null; // this being null means that the http response is not yet available, e.g. when deserializing
        private CompletableFuture<HttpResponse<String>> req = null; // this being null means that we lost it during deserialization

        public HttpResponseUD(CompletableFuture<HttpResponse<String>> req) {
            assert req != null;
            this.req = req;
        }

        public HttpResponseUD() {
            this.req = null;
        }

        private void assertValidRespObj() {
            if (req == null)
                throw new LuaJavaError("Response object persisted across serialization and is thus invalid");
        }

        @SuppressWarnings("unused")
        @LuaExposed(LuaExposed.Policy.READ)
        public final LuaProperty responseAvailable = LuaProperty.ofBoolean(() -> {
            assertValidRespObj();
            return req.isDone();
        }, null);

        @LuaCallable
        public void waitForCompletion() {
            assertValidRespObj();
            try {
                resp = req.get();
            } catch (ExecutionException e) {
                throw new LuaJavaError("An error has occurred, check the server log for more information");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LuaJavaError("An error has occurred, check the server log for more information");
            }
        }

        @LuaCallable
        public int getStatusCode() {
            waitForCompletion();
            return resp.statusCode();
        }

        @LuaCallable
        public String getResponseBody() {
            waitForCompletion();
            return resp.body();
        }

        @LuaCallable
        public int getResponseBodySize() {
            waitForCompletion();
            return resp.body().length();
        }

        @LuaCallable
        public LuaObject getResponseHeaders() {
            waitForCompletion();
            var rv = LuaObject.table();
            var headerMap = resp.headers().map();
            for (var k : headerMap.keySet()) {
                rv.set(LuaObject.of(k), LuaObject.of(String.join(",", headerMap.get(k))));
            }
            return rv;
        }

        @SuppressWarnings("unused")
        @LuaDeserializer
        public static HttpResponseUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
            return new HttpResponseUD(); // intentionally return invalid httpResponseUD object
        }

        @Override
        public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
            return new byte[0];
        }
    }
}
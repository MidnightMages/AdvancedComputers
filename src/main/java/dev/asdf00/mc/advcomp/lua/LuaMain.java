package dev.asdf00.mc.advcomp.lua;

import com.mojang.logging.LogUtils;
import dev.asdf00.jluavm.LuaVM;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class LuaMain {
    LuaVM lua;
    static String luaEntryScript = null;

    static {
        var rl = LuaMain.class.getClassLoader().getResourceAsStream("assets/advancedcomputers/lua/entry.lua");
        assert rl != null;
        luaEntryScript = new BufferedReader(new InputStreamReader(rl, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
    }

    public LuaMain() {
        try {
            lua = LuaVM.create();
            LogUtils.getLogger().info("Lua initialized successfully!");
        } catch (LinkageError ex) {
            LogUtils.getLogger().error(String.format("Failed to initialize LUA! %s", ex));
        }
    }

    public void runLuaCode() {
        // https://gudzpoz.github.io/luajava/examples/java.html

//        lua.load()

//        L.setGlobal();
//        var globals = new Globals();
//        new Lua().
//
//
//        var luaState = new LuaStateFiveThree(64*1024*1024);
//        try {
//            String file = "main.lua";
//            LuaParser parser = new LuaParser(new FileInputStream(file));
//            Chunk chunk = parser.Chunk();
//            chunk.accept( new Visitor() {
//                public void visit(Exp.NameExp exp) {
//                    System.out.println("Name in use: "+exp.name.name
//                            +" line "+exp.beginLine
//                            +" col "+exp.beginColumn);
//                }
//            } );
//        } catch ( ParseException e ) {
//            System.out.println("parse failed: " + e.getMessage() + "\n"
//                    + "Token Image: '" + e.currentToken.image + "'\n"
//                    + "Location: " + e.currentToken.beginLine + ":" + e.currentToken.beginColumn
//                    + "-" + e.currentToken.endLine + "," + e.currentToken.endColumn);
//        }

//        lua.load(classOf[Machine].getResourceAsStream(Settings.scriptPath + "machine.lua"), "=machine", "t")
    }
}

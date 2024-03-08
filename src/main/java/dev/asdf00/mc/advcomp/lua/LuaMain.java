package dev.asdf00.mc.advcomp.lua;


import com.mojang.logging.LogUtils;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import net.minecraft.resources.ResourceLocation;
import party.iroiro.luajava.AbstractLua;
import party.iroiro.luajava.lua54.Lua54;

//import dev.asdf00.mc.advcomp.repack.luakava.lua54.Lua54;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class LuaMain
{
    Lua54 lua;
    static String luaEntryScript = null;

    static {
        var rl = LuaMain.class.getClassLoader().getResourceAsStream("assets/advancedcomputers/lua/entry.lua");
        assert rl != null;
        luaEntryScript = new BufferedReader(new InputStreamReader(rl, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
    }

    public LuaMain()
    {
        try
        {
            lua = new Lua54();
            LogUtils.getLogger().info("Lua initialized successfully!");
        }
        catch (LinkageError ex)
        {
            LogUtils.getLogger().error(String.format("Failed to initialize LUA! %s", ex));
        }
    }

    private void setGlobal(String name, String value){
        lua.push(value);
        lua.setGlobal(name);
    }

    private void setGlobal(String name, Number value){
        lua.push(value);
        lua.setGlobal(name);
    }

    private void setGlobal(String name, boolean value){
        lua.push(value);
        lua.setGlobal(name);
    }

    private void setGlobal(String name, Integer value){
        lua.push(value);
        lua.setGlobal(name);
    }

    public void runLuaCode()
    {
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

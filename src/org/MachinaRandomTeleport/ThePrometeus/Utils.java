package org.MachinaRandomTeleport.ThePrometeus;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Utils {    
    // Accents Fix
    private static final Map<String,String> accents = new HashMap<String,String>() {{
        put("á", "\u00e1");
        put("à", "\u00e0");
        put("â", "\u00e2");
        put("ã", "\u00e3");
        put("ä", "\u00e4");
        put("Á", "\u00c1");
        put("À", "\u00c0");
        put("Â", "\u00c2");
        put("Ã", "\u00c3");
        put("Ä", "\u00c4");
        put("é", "\u00e9");
        put("è", "\u00e8");
        put("ê", "\u00ea");
        put("ê", "\u00ea");
        put("É", "\u00c9");
        put("È", "\u00c8");
        put("Ê", "\u00ca");
        put("Ë", "\u00cb");
        put("í", "\u00ed");
        put("ì", "\u00ec");
        put("î", "\u00ee");
        put("ï", "\u00ef");
        put("Í", "\u00cd");
        put("Ì", "\u00cc");
        put("Î", "\u00ce");
        put("Ï", "\u00cf");
        put("ó", "\u00f3");
        put("ò", "\u00f2");
        put("ô", "\u00f4");
        put("õ", "\u00f5");
        put("ö", "\u00f6");
        put("Ó", "\u00d3");
        put("Ò", "\u00d2");
        put("Ô", "\u00d4");
        put("Õ", "\u00d5");
        put("Ö", "\u00d6");
        put("ú", "\u00fa");
        put("ù", "\u00f9");
        put("û", "\u00fb");
        put("ü", "\u00fc");
        put("Ú", "\u00da");
        put("Ù", "\u00d9");
        put("Û", "\u00db");
        put("ç", "\u00e7");
        put("Ç", "\u00c7");
        put("ñ", "\u00f1");
        put("Ñ", "\u00d1");
    }};

    public static String fixAccents(String m) {
        for(Map.Entry<String, String> entry : accents.entrySet())
            m = m.replace(entry.getKey(), entry.getValue());
        
        return m;
    }
}

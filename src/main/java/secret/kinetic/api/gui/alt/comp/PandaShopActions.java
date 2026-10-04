package secret.kinetic.api.gui.alt.comp;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * The fixed set of PandaService API calls the in-game shop may run inside its page, following the shop's
 * "client login without captcha" flow: {@code POST /api/user/v1/auth/login} (or a {@code psu_live_} key, a
 * refresh, or the email 2FA code) followed by {@code POST /api/user/v1/auth/embed-token}, whose single-use
 * {@code /embed?login_token=...} URL logs the page in, so the Turnstile captcha (which fails in WebViews) is
 * never needed.
 *
 * They run in the page because its cookies (the PoW clearance) are already there. There is deliberately no way
 * to run arbitrary script: each action is a template and every parameter is inserted as an escaped JSON string.
 */
final class PandaShopActions {

    private static final Gson GSON = new Gson();

    private static final String PRELUDE =
            "(function(){"
                    + "function k(o){try{window.kinetic.result(JSON.stringify(o));}catch(e){}}"
                    + "function api(m,p,b,t){var h={'Accept':'application/json'};if(b)h['Content-Type']='application/json';"
                    + "if(t)h['Authorization']='Bearer '+t;"
                    + "return fetch(p,{method:m,headers:h,body:b?JSON.stringify(b):undefined,credentials:'include'})"
                    + ".then(function(r){return r.text().then(function(x){var j=null;try{j=JSON.parse(x);}catch(e){}"
                    // responses may wrap their payload as {success, data:{...}}; lift it so both shapes work
                    + "if(j&&j.data&&typeof j.data==='object'&&!Array.isArray(j.data)){for(var key in j.data){if(!(key in j))j[key]=j.data[key];}}"
                    + "return {status:r.status,body:j,text:x.slice(0,300)};});});}"
                    + "function fail(where,r){k({step:'error',where:where,status:r?r.status:0,body:r?(r.body||r.text):null});}"
                    + "function tok(b){b=b||{};return (b.session&&b.session.token)||b.token||'';}"
                    + "function embed(t,extra){return api('POST','/api/user/v1/auth/embed-token',null,t).then(function(r){"
                    + "if(r.body&&r.body.url){extra.step='done';k(extra);location.href=r.body.url;}else{fail('embed-token',r);}});}";
    private static final String END = "})();";

    private PandaShopActions() {
    }

    private static String q(String value) {
        return GSON.toJson(value == null ? "" : value);
    }

    /** Builds the script for one named action, or null for anything unknown. */
    static String script(String action, String[] args) {
        switch (action) {
            case "login":
                return PRELUDE
                        + "api('POST','/api/user/v1/auth/login',{username:" + q(arg(args, 0)) + ",password:" + q(arg(args, 1)) + "})"
                        + ".then(function(r){var b=r.body||{};"
                        + "if(b.requires_email_code){k({step:'code',pending_id:b.pending_id||''});return;}"
                        + "var t=tok(b);if(r.status>=300||!t){fail('login',r);return;}"
                        + "return embed(t,{token:t,refresh:b.refresh_token||'',username:" + q(arg(args, 0)) + "});})"
                        + ".catch(function(e){k({step:'error',where:'login',body:String(e)});});" + END;
            case "code":
                return PRELUDE
                        + "api('POST','/api/app/login/email-code',{pending_id:" + q(arg(args, 0)) + ",code:" + q(arg(args, 1)) + "})"
                        + ".then(function(r){var b=r.body||{};var t=tok(b);"
                        + "if(t){return embed(t,{token:t,refresh:b.refresh_token||''});}"
                        + "if(r.status<300){k({step:'done'});location.href='/embed';return;}fail('email-code',r);})"
                        + ".catch(function(e){k({step:'error',where:'email-code',body:String(e)});});" + END;
            case "key":
                return PRELUDE
                        + "embed(" + q(arg(args, 0)) + ",{key:" + q(arg(args, 0)) + "})"
                        + ".catch(function(e){k({step:'error',where:'embed-token',body:String(e)});});" + END;
            case "refresh":
                return PRELUDE
                        + "api('POST','/api/user/v1/auth/refresh',{refresh_token:" + q(arg(args, 0)) + "})"
                        + ".then(function(r){var b=r.body||{};var t=tok(b);if(!t){k({step:'expired'});return;}"
                        + "return embed(t,{token:t,refresh:b.refresh_token||" + q(arg(args, 0)) + "});})"
                        + ".catch(function(e){k({step:'error',where:'refresh',body:String(e)});});" + END;
            default:
                return null;
        }
    }

    private static String arg(String[] args, int index) {
        if (args == null || index >= args.length) return "";
        try {
            return new String(Base64.getDecoder().decode(args[index]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    /** Encodes parameters for the stdin protocol (base64, so spaces and line breaks never break a command). */
    static String encode(String value) {
        return Base64.getEncoder().encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }
}

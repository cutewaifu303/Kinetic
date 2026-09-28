#version 120

// Liquid glass panel (and plain anti-aliased rounded rects when blurMix is 0).
// Everything is in screen pixels, y up (gl_FragCoord space).

uniform sampler2D blurTex;   // blurred scene, same aspect as the screen
uniform vec2 screen;         // framebuffer size in pixels
uniform vec4 rect;           // x, y (bottom left), width, height
uniform float radius;
uniform vec4 base;           // fill colour when not blurred (rgb, alpha)
uniform vec4 tint;           // rgb, amount
uniform float blurMix;       // 0 = flat fill, 1 = glass
uniform float refraction;    // max uv shift at the rim, in pixels
uniform float rim;           // width of the lens band, in pixels
uniform float chroma;        // chromatic aberration at the rim (0..1)
uniform float specular;      // strength of the lit edge
uniform vec2 light;          // direction towards the light (unit, y up)
uniform float dim;           // constant darkening of the glass
uniform float adaptive;      // extra darkening on bright backgrounds
uniform float opacity;
uniform float shadowSize;    // outer shadow blur, pixels
uniform float shadowAlpha;
uniform float noiseAmt;
uniform float ring;          // > 0: draw only an outline this thick (pixels)

float sdRound(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 p = gl_FragCoord.xy;
    vec2 hb = rect.zw * 0.5;
    vec2 c = rect.xy + hb;
    float r = min(radius, min(hb.x, hb.y));
    vec2 q = p - c;
    float d = sdRound(q, hb, r);

    float inside = clamp(0.5 - d, 0.0, 1.0);
    if (ring > 0.0) inside *= clamp(d + ring + 0.5, 0.0, 1.0);

    // soft outer shadow, a little lower than the panel
    float shadow = 0.0;
    if (shadowAlpha > 0.0) {
        float ds = sdRound(q + vec2(0.0, shadowSize * 0.3), hb, r);
        shadow = shadowAlpha * (1.0 - smoothstep(-shadowSize * 0.2, shadowSize, ds));
    }

    if (inside <= 0.0) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, shadow * opacity);
        return;
    }

    // outward normal of the rounded rect
    float e = 0.75;
    vec2 n = vec2(sdRound(q + vec2(e, 0.0), hb, r) - sdRound(q - vec2(e, 0.0), hb, r),
                  sdRound(q + vec2(0.0, e), hb, r) - sdRound(q - vec2(0.0, e), hb, r));
    n = n / max(length(n), 1e-4);

    vec3 col = base.rgb;
    float alpha = base.a;
    if (blurMix > 0.0) {
        vec2 uv = p / screen;
        // lens: 0 in the middle, 1 at the rim, eased so the centre stays undistorted
        float t = clamp(1.0 + d / max(rim, 1.0), 0.0, 1.0);
        float bend = t * t * t;
        vec2 shift = -n * bend * refraction / screen;
        vec3 glassCol;
        if (chroma > 0.0) {
            vec2 ca = n * bend * chroma * 2.0 / screen;
            glassCol = vec3(texture2D(blurTex, uv + shift + ca).r, texture2D(blurTex, uv + shift).g, texture2D(blurTex, uv + shift - ca).b);
        } else {
            glassCol = texture2D(blurTex, uv + shift).rgb;
        }
        // adaptive darkening from the average brightness behind the whole panel
        vec3 avg = texture2D(blurTex, c / screen).rgb * 0.4;
        avg += texture2D(blurTex, (c + hb * vec2(-0.6, -0.6)) / screen).rgb * 0.15;
        avg += texture2D(blurTex, (c + hb * vec2(0.6, -0.6)) / screen).rgb * 0.15;
        avg += texture2D(blurTex, (c + hb * vec2(-0.6, 0.6)) / screen).rgb * 0.15;
        avg += texture2D(blurTex, (c + hb * vec2(0.6, 0.6)) / screen).rgb * 0.15;
        float lum = dot(avg, vec3(0.299, 0.587, 0.114));
        glassCol *= 1.0 - (dim + adaptive * smoothstep(0.28, 0.85, lum));
        col = mix(col, glassCol, blurMix);
        alpha = mix(alpha, 1.0, blurMix);
    }

    col = mix(col, tint.rgb, tint.a);

    if (specular > 0.0) {
        // faint inner shadow along the bottom
        float band = smoothstep(-7.0, 0.0, d);
        col *= 1.0 - band * max(-n.y, 0.0) * 0.22;
        // bright rim, strongest where the edge faces the light
        float lit = max(dot(n, light), 0.0);
        float edge = smoothstep(-1.8, -0.35, d);
        col += edge * (0.07 + lit * lit * specular * 0.55);
        col += smoothstep(-10.0, 0.0, d) * lit * specular * 0.05;
    }

    col += (hash(p) - 0.5) * noiseAmt;
    float a = inside * alpha * opacity;
    gl_FragColor = vec4(col, max(a, shadow * opacity * (1.0 - inside)));
}

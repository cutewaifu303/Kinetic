#version 120

// Wordmark letters: a white glyph mask coloured with a slowly wandering two-colour gradient, an
// optional shine streak, or liquid glass letters that show the blurred scene with a lit edge.

uniform sampler2D mask;
uniform sampler2D blurTex;
uniform vec2 screen;
uniform vec2 ink;        // u range of the letters inside the padded mask texture
uniform vec4 c1;
uniform vec4 c2;
uniform float phase;     // gradient offset (0..1), wanders over time
uniform float shine;     // position of the white streak (-1 = off)
uniform float glassMix;  // 1 = glass letters
uniform vec2 light;
uniform float alpha;

void main() {
    vec2 uv = gl_TexCoord[0].st;
    float a = texture2D(mask, uv).a;
    if (a <= 0.002) discard;

    float u = clamp((uv.x - ink.x) / max(ink.y - ink.x, 1e-4), 0.0, 1.0);
    float t = fract(u * 0.9 + phase);
    float tri = 1.0 - abs(2.0 * t - 1.0);
    vec3 col = mix(c1.rgb, c2.rgb, tri);

    if (glassMix > 0.0) {
        vec2 suv = gl_FragCoord.xy / screen;
        vec3 scene = texture2D(blurTex, suv).rgb;
        scene = mix(scene, col, 0.28);
        // edge lighting from the mask slope
        vec2 n = vec2(dFdx(a), dFdy(a));
        float len = length(n);
        n = len > 1e-4 ? -n / len : vec2(0.0);
        float edge = smoothstep(0.02, 0.35, len);
        float lit = max(dot(n, light), 0.0);
        scene += edge * (0.12 + lit * lit * 0.55);
        col = mix(col, scene, glassMix);
    }

    if (shine >= 0.0) {
        float s = u - (1.0 - uv.y) * 0.25;
        float streak = exp(-pow((s - shine) / 0.07, 2.0));
        col = mix(col, vec3(1.0), streak * 0.85);
    }

    gl_FragColor = vec4(col, a * alpha);
}

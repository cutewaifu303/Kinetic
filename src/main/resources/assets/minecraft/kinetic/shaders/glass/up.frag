#version 120

// Dual Kawase upsample: eight taps on a ring, written into a double size target.
uniform sampler2D tex;
uniform vec2 halfpixel;
uniform float offset;

void main() {
    vec2 uv = gl_TexCoord[0].st;
    vec3 sum = texture2D(tex, uv + vec2(-halfpixel.x * 2.0, 0.0) * offset).rgb;
    sum += texture2D(tex, uv + vec2(-halfpixel.x, halfpixel.y) * offset).rgb * 2.0;
    sum += texture2D(tex, uv + vec2(0.0, halfpixel.y * 2.0) * offset).rgb;
    sum += texture2D(tex, uv + vec2(halfpixel.x, halfpixel.y) * offset).rgb * 2.0;
    sum += texture2D(tex, uv + vec2(halfpixel.x * 2.0, 0.0) * offset).rgb;
    sum += texture2D(tex, uv + vec2(halfpixel.x, -halfpixel.y) * offset).rgb * 2.0;
    sum += texture2D(tex, uv + vec2(0.0, -halfpixel.y * 2.0) * offset).rgb;
    sum += texture2D(tex, uv + vec2(-halfpixel.x, -halfpixel.y) * offset).rgb * 2.0;
    gl_FragColor = vec4(sum / 12.0, 1.0);
}

#version 120

// Dual Kawase downsample: centre plus four diagonal taps, written into a half size target.
uniform sampler2D tex;
uniform vec2 halfpixel;
uniform float offset;

void main() {
    vec2 uv = gl_TexCoord[0].st;
    vec3 sum = texture2D(tex, uv).rgb * 4.0;
    sum += texture2D(tex, uv - halfpixel * offset).rgb;
    sum += texture2D(tex, uv + halfpixel * offset).rgb;
    sum += texture2D(tex, uv + vec2(halfpixel.x, -halfpixel.y) * offset).rgb;
    sum += texture2D(tex, uv - vec2(halfpixel.x, -halfpixel.y) * offset).rgb;
    gl_FragColor = vec4(sum / 8.0, 1.0);
}

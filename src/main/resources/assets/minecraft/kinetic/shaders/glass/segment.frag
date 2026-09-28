#version 120

// One anti-aliased line segment with round caps, in screen pixels (y up, gl_FragCoord space).
// A wide feather turns it into a soft glow.

uniform vec2 pointA;
uniform vec2 pointB;
uniform float halfWidth;   // core half width, pixels
uniform float feather;     // edge softness, pixels (about 1 for a crisp line)
uniform vec4 color;

void main() {
    vec2 p = gl_FragCoord.xy;
    vec2 pa = p - pointA;
    vec2 ba = pointB - pointA;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 0.0001), 0.0, 1.0);
    float d = length(pa - ba * h) - halfWidth;
    float alpha = 1.0 - smoothstep(-0.5 * min(feather, 1.0), feather, d);
    gl_FragColor = vec4(color.rgb, color.a * alpha);
}

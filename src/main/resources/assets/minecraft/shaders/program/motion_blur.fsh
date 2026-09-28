#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

uniform vec2 InSize;

uniform vec3 Phosphor = vec3(0.7, 0.0, 0.0);
uniform float LerpFactor = 1.0;

void main() {
    vec3 current = texture2D(DiffuseSampler, texCoord).rgb;
    vec3 previous = texture2D(PrevSampler, texCoord).rgb;
    float weight = Phosphor.r;

    vec3 mixed = mix(previous, current, weight);

    // The buffers are 8 bit: once the trail is within a colour step of the live frame the blend
    // rounds back to the old value every frame and the ghost never clears. Snap it to the live
    // frame instead, so the trail always ends.
    vec3 diff = abs(mixed - current);
    mixed = mix(mixed, current, step(diff, vec3(2.5 / 255.0)));

    gl_FragColor = vec4(mixed, 1.0);
}

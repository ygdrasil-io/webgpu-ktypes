const PI = 3.14159;
const RADIUS = 10.0;
const AREA = PI * RADIUS * RADIUS;

const VEC_CONST = vec3<f32>(1.0, 2.0, 3.0);
const VEC_LENGTH = 1.0 + 2.0;

@vertex
fn vs_main() -> @builtin(position) vec4<f32> {
    let x = AREA;
    return vec4<f32>(VEC_CONST, x);
}

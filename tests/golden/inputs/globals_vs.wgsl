var<private> counter: i32 = 0;

@vertex
fn vs_main(@location(0) pos: vec3<f32>) -> @builtin(position) vec4<f32> {
    counter = counter + 1;
    return vec4<f32>(pos, 1.0);
}

var<private> counter: i32 = 0;
var<workgroup> shared_data: array<f32, 64>;

@vertex
fn vs_main(@location(0) pos: vec3<f32>) -> @builtin(position) vec4<f32> {
    counter = counter + 1;
    return vec4<f32>(pos, 1.0);
}

@fragment
fn fs_main() -> @location(0) vec4<f32> {
    return vec4<f32>(f32(counter), 0.0, 0.0, 1.0);
}

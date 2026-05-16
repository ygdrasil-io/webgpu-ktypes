fn add_f32(a: f32, b: f32) -> f32 {
    return a + b;
}

fn sub_f32(a: f32, b: f32) -> f32 {
    return a - b;
}

fn mul_f32(a: f32, b: f32) -> f32 {
    return a * b;
}

fn div_f32(a: f32, b: f32) -> f32 {
    return a / b;
}

fn rem_f32(a: f32, b: f32) -> f32 {
    return a % b;
}

@vertex
fn vs_main(@location(0) pos: vec2<f32>) -> @builtin(position) vec4<f32> {
    let sum = add_f32(pos.x, pos.y);
    let diff = sub_f32(pos.x, pos.y);
    let prod = mul_f32(pos.x, pos.y);
    let quot = div_f32(pos.x, pos.y);
    return vec4<f32>(sum, diff, prod, quot);
}

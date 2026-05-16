struct Data {
    a: i32,
    b: vec2<f32>,
    c: array<i32, 10>,
}

struct Nested {
    data: Data,
    active: bool,
}

@group(0) @binding(0) var<storage, read_write> storage_data: Data;
@group(0) @binding(1) var<uniform> uniform_data: Nested;

@compute @workgroup_size(1)
fn main(@builtin(global_invocation_id) id: vec3<u32>) {
    let x = storage_data.a;
    storage_data.b = vec2<f32>(f32(x), 1.0);
    
    if (uniform_data.active) {
        storage_data.c[0] = uniform_data.data.a;
    }
}

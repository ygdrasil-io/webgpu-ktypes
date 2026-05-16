var<workgroup> shared_data: array<f32, 64>;

@compute @workgroup_size(64)
fn cs_main() {
    shared_data[0] = 1.0;
}

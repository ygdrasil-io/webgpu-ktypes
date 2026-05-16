fn assign_ptr(ptr: ptr<function, i32>, val: i32) {
    *ptr = val;
}

@compute @workgroup_size(1)
fn main() {
    var local_var: i32 = 0;
    assign_ptr(&local_var, 42);
}

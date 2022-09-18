window.greet = function displayuser(name, element) {
    console.log("Hi, " + name);
    element.$server.greet("aa12345");
    console.log("After Triggered DisplayUserDetails")
}

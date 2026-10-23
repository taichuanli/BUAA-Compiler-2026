int f(int a) {
    return a;
}

int main() {
    int x = 1;
    int a = 1 + switch (x) { default -> 2; } * 3;
    int b = f(switch (x) { case 1 -> 1; default -> f(switch (x) { default -> 0; }); });
    int c = (switch (x) { default -> { yield 5; } });
    switch (x) { }
    int d = switch (x) { case 1, 2, 3 -> 7; };
    while (switch (x) { default -> 1; }) {
        yield switch (x) { default -> { yield 2; } };
    }
    return a + b + c + d;
}

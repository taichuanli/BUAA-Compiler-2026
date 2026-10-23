int f(int n) {
    int i = 0, sum = 0;
    while (i < n) {
        static int k = 0;
        sum = sum + i;
        i = i + 1;
        if (i > 100) break;
        else continue;
    }
    return sum;
}

int main() {
    char c = 'a';
    int x = (int)c + 1;
    int y = f(x) + (int)2;
    int z = switch (x) {
        case 'a', 'b' -> { int t = 1; yield t; }
        case 3 -> switch (y) { default -> 7; };
        default -> 0;
    };
    switch (x) {
        case 1:
        case 2:
            x = x + 1;
            break;
        default:
            x = 0;
    }
    {
        yield;
    }
    printf("x=%d\n", x);
    return 0;
}

int main() {
    int s = 90;
    int r = switch (s) {
        case 90, 80 -> 2;
        default -> { yield 1; }
    };
    switch (s) {
        case 90: s = 0;
    }
    return 0;
}

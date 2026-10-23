int main() {
    int s = 1;
    int r = switch (s) {
        default -> { yield 1 }
    };
    return 0;
}

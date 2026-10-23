int main() {
    int s = 1;
    int r = switch (s) {
        case 1 -> 10
        case 2 -> 20
        default 30;
    };
    int t = switch (s {
        default -> 1;
    };
    switch (s) {
        case 1 s = 2;
        default: { yield }
    }
    return 0;
}

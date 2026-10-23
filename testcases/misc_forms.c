int g(int a[], int b) {
    return a[0] + b;
}

int main() {
    int arr[3];
    int i = 0;
    arr[0] = 1;
    arr[1] = 2;
    arr[i + 1] = arr[0] * 2 - 1 / 1 % 2;
    i
    = i + 1;
    int e = switch (i) { };
    int f = switch (i) {
        default -> { }
    };
    switch (i) {
        default:
            i = 0;
    }
    printf("%d %c", g(arr, i), 'z');
    return 0;
}

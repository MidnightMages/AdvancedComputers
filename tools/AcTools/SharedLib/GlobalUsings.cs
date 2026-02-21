using System.Diagnostics;
using System.Diagnostics.CodeAnalysis;

namespace SharedLib;
public static class GlobalUsings {

    [DebuggerHidden]
    public static void Assert([DoesNotReturnIf(false)] bool condition, string? message = null) {
        if (!condition) {
            throw new AssertionFailedException(message != null ? $"An assertion has failed: {message}" : "An assertion has failed!");
        }
    }
}

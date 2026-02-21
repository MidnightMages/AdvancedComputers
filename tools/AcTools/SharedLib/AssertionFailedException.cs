
namespace SharedLib;

[Serializable]
internal class AssertionFailedException : Exception {
    public AssertionFailedException() {
    }

    public AssertionFailedException(string? message) : base(message) {
    }

    public AssertionFailedException(string? message, Exception? innerException) : base(message, innerException) {
    }
}
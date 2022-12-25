namespace Application.Common.Interfaces;

public interface IOcrService
{
    Task Extract(byte[] image);
}
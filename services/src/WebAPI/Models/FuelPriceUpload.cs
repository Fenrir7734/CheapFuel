namespace WebAPI.Models;

public class FuelPriceUpload
{
    public long? FuelStationId { get; set; }
    public double? UserLongitude { get; set; }
    public double? UserLatitude { get; set; }
    public IFormFile? Image { get; set; }

    public byte[] ImageAsBytes()
    {
        if (Image is null || Image.Length <= 0) return Array.Empty<byte>();
        
        using var ms = new MemoryStream();
        Image.CopyTo(ms);
        return ms.ToArray();
    }
}
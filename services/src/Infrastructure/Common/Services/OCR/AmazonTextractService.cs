using Amazon;
using Amazon.Textract;
using Amazon.Textract.Model;
using Application.Common.Interfaces;

namespace Infrastructure.Common.Services.OCR;

public class AmazonTextractService : IOcrService
{
    public async Task Extract(byte[] image)
    {
        using var textractClient = new AmazonTextractClient(RegionEndpoint.USEast1);
        var detectResponse = await textractClient.DetectDocumentTextAsync(new DetectDocumentTextRequest
        {
            Document = new Document
            {
                Bytes = new MemoryStream(image)
            }
        });

        foreach (var block in detectResponse.Blocks)
        {
            Console.WriteLine($"Type {block.BlockType}, Text: {block.Text}");
        }
    }
}
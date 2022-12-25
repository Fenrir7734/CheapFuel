using FluentValidation;

namespace Application.FuelPrices.Commands.ExtractFuelPrices;

public class ExtractFuelPricesCommandValidator : AbstractValidator<ExtractFuelPricesCommand>
{
    private const string SupportedFileType = "image";
    private static readonly string[] SupportedFileSubtypes = { "png", "jpeg", "tiff" };
    
    public ExtractFuelPricesCommandValidator()
    {
        RuleFor(e => e.FuelStationId)
            .NotNull()
            .GreaterThanOrEqualTo(1);

        RuleFor(e => e.UserLatitude)
            .NotNull()
            .GreaterThanOrEqualTo(-90.0)
            .LessThanOrEqualTo(90.0);
        
        RuleFor(e => e.UserLongitude)
            .NotNull()
            .GreaterThanOrEqualTo(-180.0)
            .LessThanOrEqualTo(180.0);

        RuleFor(e => e.Image)
            .NotEmpty();

        RuleFor(e => e.ContentType)
            .NotNull();
        
        When(x => x.ContentType is not null, () =>
            {
                RuleFor(e => e.ContentType)
                    .Must(contentType =>
                    {
                        var parts = contentType.ToLower().Split("/");
                        var fileType = parts[0];
                        var fileSubtype = parts[1];

                        return fileType.Equals(SupportedFileType) && SupportedFileSubtypes.Contains(fileSubtype);
                    })
                    .WithMessage("Not supported file format. Currently supported formats are: " +
                                 string.Join(", ", SupportedFileSubtypes));
            });
    }
}
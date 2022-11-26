using System;
using System.Linq;
using Domain.Enums;
using Infrastructure.Persistence;

namespace WebAPI.IntegrationTests.PredefinedData;

public class UserQueryControllerData : IPredefinedData
{
    public const string User1Username = "Grzesio";
    
    public const int InitialUsersCount = 2;

    public void Seed(AppDbContext dbContext)
    {
        dbContext.Users.AddRange(GetUsers());
        dbContext.SaveChanges();
    }

    public void Clear(AppDbContext dbContext)
    {
        dbContext.Users.RemoveRange(dbContext.Users.ToList());
        dbContext.SaveChanges();
    }

    private Domain.Entities.User[] GetUsers() => new[]
    {
        new Domain.Entities.User()
        {
            Id = 200,
            Username = User1Username,
            CreatedAt = new DateTime(2022, 11, 1),
            Email = "grzesio@gmail.com",
            EmailConfirmed = true,
            MultiFactorAuthEnabled = true,
            Password = AccountsData.DefaultPasswordHash,
            Role = Role.User
        },
        new Domain.Entities.User()
        {
            Id = 201,
            Username = "Kazio",
            CreatedAt = new DateTime(2022, 12, 31),
            Email = "kazio@gmail.com",
            EmailConfirmed = true,
            MultiFactorAuthEnabled = true,
            Password = AccountsData.DefaultPasswordHash,
            Role = Role.User
        }
    };
}
using MemoryGameBackend.Models;
using Microsoft.EntityFrameworkCore;

namespace MemoryGameBackend.Data;

public class GameDbContext : DbContext
{
  public GameDbContext(DbContextOptions<GameDbContext> options) : base(options)
  {
  }

  public DbSet<User> Users => Set<User>();
  public DbSet<Score> Scores => Set<Score>();
  public DbSet<Ad> Ads => Set<Ad>();

  protected override void OnModelCreating(ModelBuilder modelBuilder)
  {
    base.OnModelCreating(modelBuilder);

    // Users table mapping
    modelBuilder.Entity<User>(entity =>
    {
      entity.ToTable("Users");
      entity.HasKey(u => u.Id);

      entity.Property(u => u.Username)
        .IsRequired()
        .HasMaxLength(100)
        .HasColumnType("varchar(100)");

      entity.Property(u => u.Password)
        .IsRequired()
        .HasMaxLength(255)
        .HasColumnName("Password")
        .HasColumnType("varchar(255)");

      entity.Property(u => u.UserType)
        .IsRequired()
        .HasMaxLength(50)
        .HasColumnType("varchar(50)");
    });

    // Scores table mapping
    modelBuilder.Entity<Score>(entity =>
    {
      entity.ToTable("Scores");
      entity.HasKey(s => s.Id);

      entity.Property(s => s.UserId)
        .IsRequired();

      entity.Property(s => s.ScoreValue)
        .IsRequired()
        .HasColumnName("Score")
        .HasColumnType("int");

      // 现有数据库无 CreatedAt 列，忽略该属性以避免查询/插入失败
      entity.Ignore(s => s.CreatedAt);

      entity.HasOne(s => s.User)
        .WithMany(u => u.Scores)
        .HasForeignKey(s => s.UserId)
        .OnDelete(DeleteBehavior.Cascade);
    });

    // Ads table mapping
    modelBuilder.Entity<Ad>(entity =>
    {
      entity.ToTable("Ads");
      entity.HasKey(a => a.Id);

      entity.Property(a => a.ImageFileName)
        .IsRequired()
        .HasMaxLength(255)
        .HasColumnType("varchar(255)");
    });
  }
}


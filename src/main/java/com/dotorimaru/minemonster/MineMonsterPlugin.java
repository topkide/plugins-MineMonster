package com.dotorimaru.minemonster;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * MineMonster - 도토리마을
 * 블록을 캐면 config.yml에 등록된 몬스터 중 하나가 랜덤으로 스폰됩니다.
 */
public final class MineMonsterPlugin extends JavaPlugin implements Listener {

    /**
     * 절대 스폰되면 안 되는 보스/금지 엔티티.
     * config.yml에 실수로 넣어도 코드 단에서 무조건 제외됩니다.
     */
    private static final Set<EntityType> BANNED = EnumSet.of(
            EntityType.WARDEN,
            EntityType.ENDER_DRAGON,
            EntityType.ELDER_GUARDIAN,
            EntityType.WITHER,
            EntityType.RAVAGER
    );

    private static final String CREATOR_MESSAGE =
            "§6[도토리마을] §f해당 플러그인은 마인크래프트 §a도토리마을§f 에서 제작된 플러그인 입니다.";

    private static final String PREFIX = "§6[MineMonster] §r";

    // ── 핫패스(BlockBreak)에서 매번 파싱하지 않도록 캐싱 ──
    private final List<EntityType> spawnPool = new ArrayList<>();
    private boolean enabled = true;
    private double spawnChance = 1.0D;   // 0.0 ~ 1.0
    private int mobsPerBreak = 1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("활성화 완료. 스폰 가능 몬스터: " + spawnPool.size() + "종");
    }

    /** config.yml을 읽어 설정/스폰풀을 캐싱한다. */
    private void loadSettings() {
        reloadConfig();
        var cfg = getConfig();

        this.enabled = cfg.getBoolean("enabled", true);

        double chance = cfg.getDouble("spawn-chance", 100.0D);
        this.spawnChance = Math.max(0.0D, Math.min(100.0D, chance)) / 100.0D;

        this.mobsPerBreak = Math.max(1, cfg.getInt("max-mobs-per-break", 1));

        spawnPool.clear();
        for (String raw : cfg.getStringList("monsters")) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            EntityType type;
            try {
                type = EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                getLogger().warning("알 수 없는 몬스터 타입 무시: " + raw);
                continue;
            }
            if (BANNED.contains(type)) {
                getLogger().warning("금지된 보스 몬스터 무시: " + type);
                continue;
            }
            if (!type.isSpawnable() || !type.isAlive()) {
                getLogger().warning("스폰 불가 엔티티 무시: " + type);
                continue;
            }
            spawnPool.add(type);
        }

        if (spawnPool.isEmpty()) {
            getLogger().warning("스폰 가능한 몬스터가 없습니다. config.yml의 monsters 목록을 확인하세요.");
        }
    }

    // ──────────────────────────────────────────────
    //  블록 채굴 → 몬스터 스폰
    // ──────────────────────────────────────────────
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // 비활성화 / 스폰풀 없음 → 즉시 반환 (가장 흔한 경로를 가장 가볍게)
        if (!enabled || spawnPool.isEmpty()) {
            return;
        }

        final ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (spawnChance < 1.0D && rng.nextDouble() >= spawnChance) {
            return;
        }

        final World world = event.getBlock().getWorld();
        // 블록 중앙 + 약간 위. 다음 틱에 블록이 공기로 바뀐 뒤 스폰되므로 질식/위치 꼬임 방지.
        final Location loc = event.getBlock().getLocation().add(0.5D, 0.0D, 0.5D);
        final int count = mobsPerBreak;

        getServer().getScheduler().runTask(this, () -> {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < count; i++) {
                EntityType type = spawnPool.get(r.nextInt(spawnPool.size()));
                world.spawnEntity(loc, type);
            }
        });
    }

    // ──────────────────────────────────────────────
    //  OP 접속 시 제작자 안내 메시지
    // ──────────────────────────────────────────────
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            player.sendMessage(CREATOR_MESSAGE);
        }
    }

    // ──────────────────────────────────────────────
    //  /minemonster on | off | reload  (OP 전용)
    // ──────────────────────────────────────────────
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("minemonster.admin")) {
            sender.sendMessage(PREFIX + "§c이 명령어를 사용할 권한이 없습니다.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(PREFIX + "현재 상태: " + (enabled ? "§aON" : "§cOFF"));
            sender.sendMessage("§7/" + label + " on | off | reload");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on" -> {
                enabled = true;
                getConfig().set("enabled", true);
                saveConfig();
                sender.sendMessage(PREFIX + "§a시스템을 켰습니다.");
            }
            case "off" -> {
                enabled = false;
                getConfig().set("enabled", false);
                saveConfig();
                sender.sendMessage(PREFIX + "§c시스템을 껐습니다.");
            }
            case "reload" -> {
                loadSettings();
                sender.sendMessage(PREFIX + "§a설정을 새로고침했습니다. (몬스터 " + spawnPool.size() + "종)");
            }
            default -> sender.sendMessage("§7/" + label + " on | off | reload");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String head = args[0].toLowerCase(Locale.ROOT);
            return Arrays.stream(new String[]{"on", "off", "reload"})
                    .filter(s -> s.startsWith(head))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}

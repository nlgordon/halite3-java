package com.insanedev

import groovy.json.JsonSlurper
import groovy.transform.Canonical
import reactor.core.publisher.Flux

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SimulationRunner {
    static void main(final String[] args) {
        // Run x number of games at one or various sizes
        // Rely on the feature flag system to handle differences in the bot behavior
        def playerCommand = "java -jar build/libs/halite3-java.jar"
        def start = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))

        def simulationResultsBySize = [:]

        for (int dimension in [32, 40, 48, 56, 64]) {
            println("Starting on dimension $dimension")
            def replayDir = "replays/$start/$dimension"
            new File(replayDir).mkdirs()

            def command = ["./halite", "--results-as-json", "--no-logs", "-i", replayDir, "--width", dimension, "--height", dimension, playerCommand, playerCommand]

            def gameResults = []
            for (int i = 0; i < 11; i++) {
                println("Starting run $i")
                def execution = command.execute()
                execution.waitFor()
                def errorText = execution.getErr().text
                if (!errorText.isAllWhitespace()) {
                    println(errorText)
                }
                def output = execution.text
                def results = new JsonSlurper().parseText(output)
                new File("bot-0.log").renameTo("$replayDir/$i-bot-0.log")
                new File("bot-1.log").renameTo("$replayDir/$i-bot-1.log")

                println("Player results")
                results.stats.forEach({ String key, Map result -> println("Player: $key Rank: $result.rank Score: $result.score") })
                def gameResult = Flux.fromIterable(results.stats.keySet()).reduce(new GameResult(i), { GameResult result, String it ->
                    def playerResult = results.stats[it]
                    def id = it.toInteger()
                    result.playerResults[id] = new PlayerResult(playerId: id, score: playerResult.score, rank: playerResult.rank)
                    return result
                }).block()
                println(gameResult)
                gameResults << gameResult
            }

            def simulationResult = Flux.fromIterable(gameResults).reduce(new SimulationResult(2, dimension), {SimulationResult simulationResult, GameResult game ->
                simulationResult.playerWinCount[game.winningPlayerId]++
                simulationResult.winPercentages[game.winningPlayerId] += game.winningScorePercentage
                return simulationResult
            }).block()

            println(simulationResult)
            println("Player $simulationResult.winningPlayer wins at dimension $dimension")
            simulationResultsBySize[dimension] = simulationResult
        }

        Flux.fromIterable(simulationResultsBySize.keySet()).sort({Integer left, Integer right ->
            left <=> right
        }).subscribe({
            def result = simulationResultsBySize[it]
            println(result)
        })
    }
}

@Canonical
class SimulationResult {
    Map<Integer, Integer> playerWinCount = [:]
    Map<Integer, BigDecimal> winPercentages = [:]
    Integer dimension

    SimulationResult(int players, int dimension) {
        Flux.range(0, players).subscribe({playerWinCount[it] = 0; winPercentages[it] = 0.0})
        this.dimension = dimension
    }

    Integer getWinningPlayer() {
        Flux.fromIterable(playerWinCount.keySet())
                .sort({Integer left, Integer right -> playerWinCount[right] <=> playerWinCount[left]})
                .blockFirst()
    }
}

@Canonical
class GameResult {
    Integer game
    Map<Integer, PlayerResult> playerResults = [:]

    GameResult(Integer game) {
        this.game = game
    }

    Integer getWinningPlayerId() {
        return Flux.fromIterable(playerResults.values())
                .filter({it.rank == 1})
                .map({it.playerId})
                .blockFirst()
    }

    Integer getLosingPlayerId() {
        return Flux.fromIterable(playerResults.values())
                .filter({it.rank == 2})
                .map({it.playerId})
                .blockFirst()
    }

    BigDecimal getWinningScorePercentage() {
        (playerResults[winningPlayerId].score - playerResults[losingPlayerId].score) / (playerResults[0].score + playerResults[1].score) * 100
    }
}

@Canonical
class PlayerResult {
    Integer playerId
    Integer score
    Integer rank
}

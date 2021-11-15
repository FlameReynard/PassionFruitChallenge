import Analysis
import Preparation
import time
from Trainer import Trainer

if __name__ == '__main__':
    # Analysis.analyse()
    stats_file_name = "record.txt"
    stats_file = open(stats_file_name, "w+")
    stats_file.truncate()
    stats_file.close()
    for i in range(10):
        run = i + 1
        print("Preparing and splitting the dataset...")
        Preparation.prepare("AugmentedImages", False)  # Use Train_Images when testing unmodified images
        trainer = Trainer(20, 0.82, 0.004, 0.7, 0, stats_file_name)
        epochs = 50

        print("Beginning training...")
        start_time = time.perf_counter()
        for t in range(epochs):
            print(f"Epoch {t + 1}\n-------------------------------")
            trainer.train()
            trainer.test()
            if trainer.converged:
                break

        end_time = time.perf_counter()
        timer = end_time - start_time

        print("Training finished, recording results...")
        stats_file = open(stats_file_name, "a")
        stats_file.write("=====================================\n")
        stats_file.write(f"Run: {run:f}\n")
        stats_file.write("=====================================\n")
        stats_file.write(f"Time: {timer:f}\n")
        stats_file.close()

        trainer.record()
        print(f"Time: {timer: 0.4f}\n")

        print("Evaluating converged model")
        trainer.evaluate("converged.pth")
        print("Evaluating best model")
        trainer.evaluate("bestModel.pth")

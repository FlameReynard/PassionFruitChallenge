import statistics

from torch import nn
from torch.optim import SGD
from torch.utils.data import DataLoader

import CNN
import torch

import DataSet

device = "cuda" if torch.cuda.is_available() else "cpu"


class Trainer:

    def __init__(self, batch_size, threshold, learning_rate, momentum, weight_decay, stats_file_name):
        print("Using {} device".format(device))
        train_set = DataSet.CustomImageDataset("training_classes.csv", "Training")
        generalise_set = DataSet.CustomImageDataset("generalising_classes.csv", "Generalising")
        validate_set = DataSet.CustomImageDataset("validating_classes.csv", "Validating")

        self.model = CNN.NeuralNetwork().to(device)
        self.training_loader = DataLoader(train_set, batch_size=batch_size, shuffle=True)
        self.testing_loader = DataLoader(generalise_set, batch_size=batch_size, shuffle=True)
        self.validating_loader = DataLoader(validate_set, batch_size=batch_size, shuffle=True)
        self.loss_fn = nn.CrossEntropyLoss()
        self.optimizer = SGD(self.model.parameters(), lr=learning_rate, momentum=momentum, weight_decay=weight_decay)
        self.accuracy = 0
        self.error = 0
        self.best_accuracy = 0
        self.number_of_epochs = 0
        self.best_epoch = -1
        self.best_accuracy_error = -1
        self.converged = False
        self.threshold = threshold
        self.accuracies = []
        self.errors = []
        self.filename = stats_file_name

    def measure(self, loader):
        size = len(loader.dataset)
        num_batches = len(loader)
        self.model.eval()
        error, correct = 0, 0
        with torch.no_grad():
            for x, y in loader:
                x, y = x.to(device), y.to(device)
                pred = self.model(x)
                error += self.loss_fn(pred, y).item()
                correct += (pred.argmax(1) == y).type(torch.float).sum().item()
        error /= num_batches
        correct /= size
        accuracy = 100 * correct
        print(f"Test Error: \n Accuracy: {accuracy :>0.1f}%, Avg loss: {error:>8f} \n")
        return accuracy, error

    def train(self):
        self.model.train()

        size = len(self.training_loader.dataset)
        for batch, (x, y) in enumerate(self.training_loader):
            x, y = x.to(device), y.to(device)

            # Compute prediction error
            pred = self.model(x)
            loss = self.loss_fn(pred, y)

            # Backpropagation
            self.optimizer.zero_grad()
            loss.backward()
            self.optimizer.step()

            if batch % 10 == 0:
                loss, current = loss.item(), batch * len(x)
                print(f"loss: {loss:>7f}  [{current:>5d}/{size:>5d}]")

    def test(self):
        accuracy, error = self.measure(self.testing_loader)
        print(f"Test Error: \n Accuracy: {accuracy :>0.1f}%, Avg loss: {error:>8f} \n")
        self.accuracies.append(accuracy)
        self.errors.append(error)
        self.number_of_epochs += 1
        self.accuracy = accuracy
        self.error = error
        if error < self.threshold:
            self.converged = True
        if accuracy > self.best_accuracy:
            self.best_accuracy = accuracy
            self.best_epoch = self.number_of_epochs
            self.best_accuracy_error = error
            torch.save(self.model.state_dict(), "bestModel.pth")

    def record(self):
        accuracy, error = self.measure(self.training_loader)
        record_file = open(self.filename, "a")
        record_file.write(self.filename)
        record_file.write(f"Best accuracy: {self.best_accuracy:f}\n")
        record_file.write(f"Best accuracy epoch: {self.best_epoch:f}\n")
        record_file.write(f"Best accuracy error: {self.best_accuracy_error:f}\n")
        record_file.write("-------------------------------------\n")
        record_file.write(f"Training accuracy: {accuracy:f}\n")
        record_file.write(f"Training error: {error:f}\n")
        record_file.write("-------------------------------------\n")
        record_file.write(f"Generalisation accuracy: {self.accuracy:f}\n")
        record_file.write(f"Generalisation error: {self.error:f}\n")
        record_file.write(f"Epochs: {self.number_of_epochs:f}\n")
        record_file.write("-------------------------------------\n")
        record_file.write(f"Average accuracy: {statistics.mean(self.accuracies):f}\n")
        record_file.write(f"Average error: {statistics.mean(self.errors):f}\n")
        record_file.write(f"Stdev accuracy: {statistics.stdev(self.accuracies):f}\n")
        record_file.write(f"Stdev error: {statistics.stdev(self.errors):f}\n")
        record_file.close()
        torch.save(self.model.state_dict(), "converged.pth")

    def evaluate(self, model):
        self.model = CNN.NeuralNetwork().to(device)
        self.model.load_state_dict(torch.load(model))
        accuracy, error = self.measure(self.validating_loader)
        print(f"Validation Error: \n Accuracy: {accuracy :>0.1f}%, Avg loss: {error:>8f} \n")

        record_file = open(self.filename, "a")
        record_file.write("-------------------------------------\n")
        record_file.write(model + "\n")
        record_file.write(f"Final accuracy: {accuracy:f}\n")
        record_file.write(f"Final error: {error:f}\n")

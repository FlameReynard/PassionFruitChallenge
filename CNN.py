from torch import nn


class NeuralNetwork(nn.Module):
    def __init__(self):
        super(NeuralNetwork, self).__init__()
        self.architecture = nn.Sequential(
            nn.Conv2d(3, 6, (5, 5)),
            nn.AdaptiveAvgPool2d((300, 300)),
            nn.ReLU(),
            nn.Conv2d(6, 10, (3, 3)),
            nn.AdaptiveAvgPool2d((200, 200)),
            nn.ReLU(),
            nn.Conv2d(10, 10, (5, 5)),
            nn.AdaptiveAvgPool2d((100, 100)),
            nn.ReLU(),
            nn.Conv2d(10, 16, (3, 3)),
            nn.AdaptiveAvgPool2d((20, 20)),
            nn.ReLU(),
            nn.Conv2d(16, 16, (3, 3)),
            nn.AdaptiveAvgPool2d((10, 10)),
            nn.ReLU(),
            nn.Flatten(),
            nn.Linear(1600, 1600),
            nn.ReLU(),
            nn.Linear(1600, 1600),
            nn.ReLU(),
            nn.Linear(1600, 600),
            nn.ReLU(),
            nn.Linear(600, 250),
            nn.ReLU(),
            nn.Linear(250, 20),
            nn.ReLU(),
            nn.Linear(20, 3),
            nn.Softmax(),
        )

    def forward(self, x):
        architecture = self.architecture(x)
        return architecture

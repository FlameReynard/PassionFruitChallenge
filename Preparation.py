import os
import random
import shutil
from PIL import Image

def prepare(source_folder, crop):
    metadata_file = open("Train.csv")
    metadata = metadata_file.readlines()

    classes = []
    skip = True
    for line in metadata:
        if skip:
            skip = False
            continue
        row = line.split(",")
        if not classes.__contains__(row[1]):
            classes.append(row[1])

    target_files = [
        open("training_classes.csv", "w+"),
        open("generalising_classes.csv", "w+"),
        open("validating_classes.csv", "w+")
    ]
    target_folders = ["Training", "Generalising", "Validating"]
    target_counts = [0, 0, 0]

    for folder in target_folders:
        try:
            os.mkdir(folder)
        except FileExistsError:
            for file in os.scandir(folder):
                os.remove(file.path)

    number_of_instances = len(os.listdir(source_folder)) - 1
    train_amount = 0.6 * number_of_instances
    generalise_amount = 0.2 * number_of_instances
    validation_amount = 0.2 * number_of_instances

    added_files = []
    skip = True
    for line in metadata:
        if skip:
            skip = False
            continue

        row = line.split(",")
        image_name = row[0] + ".jpg"
        class_number = classes.index(row[1])

        if image_name in added_files:
            continue
        else:
            added_files.append(image_name)

        bottom = 0
        top = 2
        exclude = 1 if target_counts[1] == generalise_amount else -1
        if target_counts[0] == train_amount:
            bottom = 1

        if target_counts[2] == validation_amount:
            top = 1

        if bottom == exclude and top == exclude:
            bottom = 0
            top = 0  # add any extra to the training set

        folder_number = random.randint(bottom, top)
        while folder_number == exclude:
            folder_number = random.randint(bottom, top)

        target_counts[folder_number] = target_counts[folder_number] + 1
        target_folder = target_folders[folder_number]
        target_file = target_files[folder_number]

        target_file.write(image_name + ",%d" % class_number + "\n")
        if not crop:
            shutil.copy(source_folder + "/" + image_name, target_folder + "/" + image_name)
        else:
            x = row[2]
            y = row[3]
            width = row[4]
            height = row[5]
            image = Image.open(source_folder + "/" + image_name)
            image = image.copy().crop((x, y, x+width, y+height))
            image.save(target_folder + "/" + image_name)
